# minute manager

## Prerequisites

- Java
- Leiningen
- Yarn

## Running the tests

lein cloverage

## Running the app

0. lein deps
1. yarn install
2. lein run migrate
3. yarn run shadow-cljs watch app 
4. lein run (or (user/start) from repl)

## Deploying the app

0. lein uberjar
1. export DATABASE_URL="jdbc:h2:./minuteman.db"
2. lein migrate
3. java -jar target/uberjar/minuteman.jar
