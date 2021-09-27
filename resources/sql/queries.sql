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

-- :name delete-test-instances! :! :n
-- :doc deletes test es instances
delete from es_instance
where name = 'test_name'

-- :name create-es-instance! :insert :raw
-- :doc create an es instance record
insert into es_instance
(name, url, headers)
values (:name, :url, :headers)

-- :name get-es-instances :? :*
-- :doc retrieves all es instances
select * from es_instance

-- :name get-es-instance :? :1
-- :doc retrieves all es instances
select * from es_instance where id = :id

-- :name create-es-index! :insert :raw
-- :doc create an es index record
insert into es_index
(name, es_instance_id)
values (:name, :es_instance_id)

-- :name get-es-indices :? :*
-- :doc retrieves all es indices with current states
select idx.name
     , idx.es_instance_id
     , idx.id
     , a.health
     , a.docs_count
     , a.docs_deleted
     , a.store_size
     , a.created as updated
  from es_index idx
     , es_index_state a
     , (select es_index_id, max(created) as created
          from es_index_state
         group by es_index_id) b
 where idx.id = a.es_index_id
   and a.created = b.created
   and a.es_index_id = b.es_index_id

-- :name get-es-index :? :1
-- :doc retrieves an es index
select * from es_index where id = :id

-- :name watch-es-index! :! :n
-- :doc set watch for index
update es_index set watching = true where id = :id

-- :name unwatch-es-index! :! :n
-- :doc set watch for index
update es_index set watching = false where id = :id

-- :name create-es-index-state! :insert :raw
-- :doc create an es index state record
insert into es_index_state
(name, es_index_id, health, docs_count, docs_deleted, store_size)
values (:name, :es_index_id, :health, :docs_count, :docs_deleted, :store_size)

-- :name get-index-states :? :*
-- :doc retrieves index states
select * from es_index_state where es_index_id = :es_index_id

-- :name get-current-index-state :? :1
-- :doc retrieves current index state
select a.*
  from es_index_state a
     , (select es_index_id, max(created) as created
          from es_index_state
         group by es_index_id) b
 where a.created = b.created
   and a.es_index_id = b.es_index_id
   and a.es_index_id = :es_index_id

-- :name create-es-load-monitor! :insert :raw
-- :doc create an es batch load monitor record
insert into es_load_monitor
(name, status, start_state_id, end_state_id)
values (:name, :status, :start_state_id, :end_state_id)
