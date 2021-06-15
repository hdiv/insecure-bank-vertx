# Insecure Bank
![Insecure-Bank](https://hdivsecurity.com/img/bank.png)
## Running the application locally

1. Clone the repository:

        $ git clone https://github.com/hdiv/insecure-bank-vertx.git

2. Run the application using an embedded Tomcat:

        $ mvnw clean package
        $ mvnw cargo:run

3. You can then access the bank application here: http://localhost:8080

## Running with Docker

Run the insecure-bank application with Docker.

Place Hdiv agent and license in the ``agent`` root folder.

        $ docker-compose build insecure-bank
        $ docker-compose up insecure-bank

Open the application in > http://localhost:8080

## Login credentials
- Username: john
- Password: test