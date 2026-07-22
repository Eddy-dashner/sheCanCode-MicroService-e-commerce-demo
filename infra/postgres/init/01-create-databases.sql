-- One database per service. Services never share tables or a schema, so we
-- provision an isolated database for each one up front. Later phases add users
-- and grants; for the learning build every service uses the shared 'shop' role.
CREATE DATABASE users_db;
CREATE DATABASE product_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
