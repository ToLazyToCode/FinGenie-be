UPDATE chat_message
SET message_type = 'TEXT'
WHERE message_type IS NULL;

UPDATE chat_message
SET is_read = FALSE
WHERE is_read IS NULL;
