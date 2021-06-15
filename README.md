# Insecure Bank
![Insecure-Bank](https://hdivsecurity.com/img/bank.png)
## Running the application locally

1. Clone the repository:

        $ git clone https://github.com/hdiv/insecure-bank-vertx.git

2. Run the application using an embedded Tomcat:

        $ ./mvnw clean package
        $ ./mvnw quarkus:dev

3. You can then access the bank application here: http://localhost:8080

## Running with Docker

Run the insecure-bank application with Docker:

        $ docker-compose build
        $ docker-compose up insecure-bank

Open the application in > http://localhost:8080

If you want to run the application with the agent enabled place Hdiv agent and license in the ``agent`` root folder and run:

        $ docker-compose up insecure-bank-agent

## Login credentials
- Username: john
- Password: test

