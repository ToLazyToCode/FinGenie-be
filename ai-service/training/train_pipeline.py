"""
FinGenie AI Training Pipeline
"""
import os
import json
import logging
from datetime import datetime
from typing import Dict, Any, List, Optional

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, GradientBoostingRegressor
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import mean_squared_error, mean_absolute_error, r2_score
import joblib
import mlflow
import mlflow.sklearn

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class SpendingPredictorTrainer:
    """
    Training pipeline for spending predictor model.
    
    Features:
    - Cross-validation for model selection
    - MLflow experiment tracking
    - Model versioning and registry
    - Automatic metrics logging
    """
    
    FEATURES = [
        "rolling_7d_spend",
        "volatility_score",
        "tracking_frequency",
        "avg_transaction_amount",
        "transaction_count_7d",
        "weekday_vs_weekend_ratio",
        "emotional_spending_score"
    ]
    
    TARGET = "actual_daily_spend"
    
    def __init__(
        self,
        mlflow_tracking_uri: str = "http://localhost:5000",
        experiment_name: str = "fingenie-spending-predictor"
    ):
        self.mlflow_tracking_uri = mlflow_tracking_uri
        self.experiment_name = experiment_name
        self.model = None
        self.model_version = None
        
        # Setup MLflow
        mlflow.set_tracking_uri(mlflow_tracking_uri)
        mlflow.set_experiment(experiment_name)
    
    def prepare_data(
        self,
        df: pd.DataFrame
    ) -> tuple[pd.DataFrame, pd.Series]:
        """Prepare features and target from raw data."""
        # Ensure all features exist
        for feature in self.FEATURES:
            if feature not in df.columns:
                logger.warning(f"Missing feature: {feature}, setting to 0")
                df[feature] = 0
        
        X = df[self.FEATURES]
        y = df[self.TARGET]
        
        # Handle missing values
        X = X.fillna(0)
        y = y.fillna(y.median())
        
        return X, y
    
    def train(
        self,
        df: pd.DataFrame,
        model_type: str = "gradient_boosting",
        hyperparams: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        Train the spending predictor model.
        
        Args:
            df: Training data with features and target
            model_type: "random_forest" or "gradient_boosting"
            hyperparams: Optional hyperparameters override
        
        Returns:
            Training metrics dictionary
        """
        X, y = self.prepare_data(df)
        
        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42
        )
        
        # Default hyperparameters
        default_params = {
            "random_forest": {
                "n_estimators": 100,
                "max_depth": 10,
                "min_samples_split": 5,
                "min_samples_leaf": 2,
                "random_state": 42
            },
            "gradient_boosting": {
                "n_estimators": 100,
                "max_depth": 5,
                "learning_rate": 0.1,
                "min_samples_split": 5,
                "random_state": 42
            }
        }
        
        params = hyperparams or default_params.get(model_type, {})
        
        # Create model
        if model_type == "random_forest":
            self.model = RandomForestRegressor(**params)
        else:
            self.model = GradientBoostingRegressor(**params)
        
        # Train with MLflow tracking
        with mlflow.start_run() as run:
            # Log parameters
            mlflow.log_params(params)
            mlflow.log_param("model_type", model_type)
            mlflow.log_param("feature_count", len(self.FEATURES))
            mlflow.log_param("training_samples", len(X_train))
            
            # Train
            logger.info(f"Training {model_type} model...")
            self.model.fit(X_train, y_train)
            
            # Evaluate
            y_pred = self.model.predict(X_test)
            
            metrics = {
                "rmse": np.sqrt(mean_squared_error(y_test, y_pred)),
                "mae": mean_absolute_error(y_test, y_pred),
                "r2": r2_score(y_test, y_pred),
                "mape": np.mean(np.abs((y_test - y_pred) / (y_test + 1))) * 100
            }
            
            # Cross-validation
            cv_scores = cross_val_score(
                self.model, X, y, cv=5, scoring="neg_mean_squared_error"
            )
            metrics["cv_rmse_mean"] = np.sqrt(-cv_scores.mean())
            metrics["cv_rmse_std"] = np.sqrt(-cv_scores).std()
            
            # Feature importance
            feature_importance = dict(zip(
                self.FEATURES,
                self.model.feature_importances_.tolist()
            ))
            
            # Log metrics
            mlflow.log_metrics(metrics)
            mlflow.log_dict(feature_importance, "feature_importance.json")
            
            # Log model
            mlflow.sklearn.log_model(
                self.model,
                "model",
                registered_model_name="spending_predictor"
            )
            
            self.model_version = f"v{datetime.now().strftime('%Y%m%d_%H%M%S')}"
            mlflow.log_param("model_version", self.model_version)
            
            logger.info(f"Training complete. Run ID: {run.info.run_id}")
            logger.info(f"Metrics: RMSE={metrics['rmse']:.2f}, MAE={metrics['mae']:.2f}, R2={metrics['r2']:.4f}")
            
            return {
                "run_id": run.info.run_id,
                "model_version": self.model_version,
                "metrics": metrics,
                "feature_importance": feature_importance
            }
    
    def save_model(self, path: str):
        """Save model to disk."""
        if self.model is None:
            raise ValueError("No model to save")
        
        filename = f"spending_predictor_{self.model_version}.joblib"
        filepath = os.path.join(path, filename)
        
        os.makedirs(path, exist_ok=True)
        joblib.dump(self.model, filepath)
        logger.info(f"Model saved to {filepath}")
        
        return filepath
    
    def load_model(self, path: str, version: str):
        """Load model from disk."""
        filename = f"spending_predictor_{version}.joblib"
        filepath = os.path.join(path, filename)
        
        self.model = joblib.load(filepath)
        self.model_version = version
        logger.info(f"Model loaded from {filepath}")
    
    def evaluate_drift(
        self,
        baseline_df: pd.DataFrame,
        current_df: pd.DataFrame
    ) -> Dict[str, float]:
        """
        Evaluate data drift between baseline and current data.
        
        Returns PSI (Population Stability Index) for each feature.
        """
        drift_scores = {}
        
        for feature in self.FEATURES:
            if feature in baseline_df.columns and feature in current_df.columns:
                psi = self._calculate_psi(
                    baseline_df[feature].values,
                    current_df[feature].values
                )
                drift_scores[feature] = psi
        
        return drift_scores
    
    def _calculate_psi(
        self,
        baseline: np.ndarray,
        current: np.ndarray,
        bins: int = 10
    ) -> float:
        """Calculate PSI (Population Stability Index)."""
        # Create bins from baseline
        edges = np.percentile(baseline, np.linspace(0, 100, bins + 1))
        edges[0] = -np.inf
        edges[-1] = np.inf
        
        # Calculate distributions
        baseline_counts = np.histogram(baseline, bins=edges)[0]
        current_counts = np.histogram(current, bins=edges)[0]
        
        # Normalize
        baseline_pct = baseline_counts / len(baseline) + 0.0001
        current_pct = current_counts / len(current) + 0.0001
        
        # Calculate PSI
        psi = np.sum((current_pct - baseline_pct) * np.log(current_pct / baseline_pct))
        
        return psi


def generate_synthetic_data(n_samples: int = 1000) -> pd.DataFrame:
    """Generate synthetic training data for testing."""
    np.random.seed(42)
    
    data = {
        "rolling_7d_spend": np.random.exponential(500000, n_samples),
        "volatility_score": np.random.uniform(0, 1, n_samples),
        "tracking_frequency": np.random.poisson(3, n_samples),
        "avg_transaction_amount": np.random.exponential(100000, n_samples),
        "transaction_count_7d": np.random.poisson(10, n_samples),
        "weekday_vs_weekend_ratio": np.random.uniform(0.5, 2, n_samples),
        "emotional_spending_score": np.random.beta(2, 5, n_samples),
    }
    
    # Generate target with some noise
    df = pd.DataFrame(data)
    df["actual_daily_spend"] = (
        df["rolling_7d_spend"] / 7 * 
        (1 + df["volatility_score"] * 0.3) *
        (1 + df["emotional_spending_score"] * 0.2) +
        np.random.normal(0, 50000, n_samples)
    ).clip(lower=0)
    
    return df


if __name__ == "__main__":
    # Example training run
    logger.info("Generating synthetic data...")
    df = generate_synthetic_data(5000)
    
    logger.info("Starting training pipeline...")
    trainer = SpendingPredictorTrainer(
        mlflow_tracking_uri="http://localhost:5000",
        experiment_name="fingenie-spending-predictor"
    )
    
    results = trainer.train(df, model_type="gradient_boosting")
    
    logger.info(f"Training results: {json.dumps(results, indent=2, default=str)}")
    
    # Save model
    trainer.save_model("/app/models")
