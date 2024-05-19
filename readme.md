# TurnWithoutSignal

This project is a Java application that allows two users to exchange messages over a network using the DatagramSocket API.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

- Java 8 or higher
- Maven

### Installing

1. Clone the repository
2. Navigate to the project directory
3. Run `mvn install` to install the necessary dependencies

## Running the Application

You can run the application by executing the `User1` and `User2` classes in the `com.safeai` package. 

- Run `User1` class: This will initialize a `StreamClient` at port 9000 and wait for `User2` to connect.
- Run `User2` class: This will initialize a `StreamClient` at port 18888 and connect to `User1`.
- Wait until "Enter Message" is displayed on the console.

Both users can then exchange messages.

## Built With

- [Java](https://www.java.com) - The programming language used
- [Maven](https://maven.apache.org/) - Dependency Management

