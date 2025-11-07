# Football Reverse Betting System

*A decentralized platform for reverse betting on football matches.*

## Overview

This project provides a platform for users to participate in reverse betting on football games. The system leverages blockchain technology and smart contracts to ensure transparency and fairness. It is built using Java (JDK 21) and utilizes Alchemy for payment notifications.

## Key Features

*   **Reverse Betting:** Allows users to bet against the outcome of a match.
*   **ETH-USDT Support:** Currently supports ETH-USDT trading pair.
*   **Alchemy Integration:** Uses Alchemy for secure payment callbacks.
*   **Redis Storage:** Wallet information is securely stored in Redis.
*   **Admin Panel:** A dedicated admin panel for system configuration and management.

## Prerequisites

*   Docker and Docker Compose installed.
*   Java JDK 21 installed.
*   An Alchemy account with API keys.
*   Redis server running.

## Setup & Deployment

1.  **Start Base Services:** Navigate to the `.docker` directory and run:
    ```bash
    docker compose up
    ```
2.  **Admin Panel Setup:**
    *   Start the admin panel application.
    *   Default username: `admin`
    *   Default password: `admin`
    *   Configure Alchemy API keys within the admin panel.
3.  **Enable System:** In the system settings within the admin panel, ensure "System Enabled" is configured to `true`.
4.  **Deploy `veto-boot` Service:** Deploy the `veto-boot` service.
5.  **Static Page Deployment:** Compile the static pages and mount them to the designated container (refer to container repository: `xxx`).

## Usage

1.  **Select ETH-USDT:** After the system is running, ensure the ETH-USDT currency pair is selected in the system settings.
2.  **Wallet Management:** Wallet information is automatically managed and stored in Redis.
3.  **Reverse Betting:** Users can participate in reverse betting through the frontend interface.

## Technology Stack

*   **Language:** Java (JDK 21)
*   **Framework:** (Please specify the framework used, e.g., Spring Boot)
*   **Database:** Redis
*   **Blockchain Integration:** Alchemy
*   **Containerization:** Docker, Docker Compose

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).

## Chinese Version

For the Chinese version of this README, please see [README_cn.md](README_cn.md).