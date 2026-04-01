@echo off
cd /d "D:\code lord\supper vibe\FinGenie\FinGenie---be"

set "POSTGRES_DB=fingenie"
set "POSTGRES_USER=fingenie"
set "POSTGRES_PASSWORD=fingenie"
set "REDIS_PASSWORD=fingenie-redis-2024"
set "GEMINI_API_KEY=AIzaSyBTocbg1SOqOZpCjgxT6OwgZQRoP2zMIMw"
set "GEMINI_MODEL=gemini-2.5-flash"
set "LLM_TIMEOUT_SECONDS=120"
set "MAIL_USERNAME=hungphucvp7@gmail.com"
set "MAIL_PASSWORD=pdfd jaed ytdh kvcv"
set "JWT_SECRET=583U_zCUihu3AC18Ycg26d40XNsVlN67-rpbbLeYdH-jj3ogPCBlIouzBmsRrcI7ImyBZX9RwIh146EJ0dwnaB_gZUKIuzKil9ivKbmKMhmn7JLPSgImL-7fLVuzrSsk922AudzigQldFA7MBKR85se24kbrP4AtRGsD9Cdnmrblm874XaiM2zKSt9V7vzXJ9k4fTB-_-ZI1RqHBBzxh9YgnLqAVclxixJA7L2bcfG_fJtqLfFDU3sK73HIo4OrKdSfroI7Ilkq97vKfX5hhEZaeE9qhdFsidy1rnjTzrT89ObnPdmm8XSYo4eollf7MBGVydkLTQb50nKTN1dpCYA"
set "SPRING_MAIL_USERNAME=%MAIL_USERNAME%"
set "SPRING_MAIL_PASSWORD=%MAIL_PASSWORD%"

echo Starting Spring Boot backend with env vars...
call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local > backend.log 2>&1
