-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, first_name, last_name, email, pass)
VALUES (:id, :first_name, :last_name, :email, :pass)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name create-es-instance! :! :n
-- :doc create an es instance record
insert into es_instance
(name, url, headers)
values (:name, :url, :headers)

-- :name get-es-instances :? :*
-- :doc retrieves all es instances
select * from es_instance

-- :name create-es-index! :! :n
-- :doc create an es index record
insert into es_index
(name, es_instance_id)
values (:name, es_instance_id)

-- :name get-es-indices :? :*
-- :doc retrieves all es indices
select * from es_index

-- :name watch-es-index! :! :n
-- :doc set watch for index
update es_index set watch = true where id = :id

-- :name unwatch-es-index! :! :n
-- :doc set watch for index
update es_index set watch = false where id = :id

-- :name create-es-index-state! :! :n
-- :doc create an es index state record
insert into es_index_state
(name, es_index_id, health, docs_count, docs_deleted, store_size, created)
values (name, :es_index_id, :health, :docs_count, :docs_deleted, :store_size, :created)

-- :name create-es-load-monitor! :! :n
-- :doc create an es batch load monitor record
insert into es_load_monitor
(name, status, start_state_id, end_state_id)
values (:name, :status, :start_state_id, :end_state_id)

