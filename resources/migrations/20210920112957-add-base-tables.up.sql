CREATE TABLE users
(id VARCHAR(20) PRIMARY KEY,
 first_name VARCHAR(30),
 last_name VARCHAR(30),
 email VARCHAR(30),
 admin BOOLEAN,
 last_login TIMESTAMP,
 is_active BOOLEAN,
 pass VARCHAR(300));

CREATE TABLE es_instance (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(20) NOT NULL,
  url VARCHAR(255) NOT NULL,
  headers VARCHAR(510));

CREATE TABLE es_index (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  watching BOOLEAN NOT NULL DEFAULT FALSE,
  es_instance_id BIGINT NOT NULL,
  FOREIGN KEY (es_instance_id) REFERENCES es_instance(id));

CREATE TABLE es_index_state (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(20) NOT NULL,
  es_index_id BIGINT NOT NULL,
  health VARCHAR(20),
  docs_count BIGINT,
  docs_deleted BIGINT,
  store_size VARCHAR(20),
  created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (es_index_id) REFERENCES es_index(id));

CREATE TABLE es_load_monitor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  start_state_id BIGINT,
  end_state_id BIGINT,
  FOREIGN KEY (start_state_id) REFERENCES es_index_state(id),
  FOREIGN KEY (end_state_id) REFERENCES es_index_state(id));
