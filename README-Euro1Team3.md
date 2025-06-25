# Class of 2025 Euro 1 Team 3 — Project Overview

**Blockchain Engineering (CS4160) — TU Delft**

---

## Authors

- **Jan Bryczkowski** (`5489903`)
- **Stanisław Howard** (`5533368`)
- **Filip Błaszczyk** (`5575958`)
- **Daniel Lihotský** (`5518199`)
- **Christiaan Molier** (`6334385`)

---

## Introduction

This document provides an overview of the project developed for the Blockchain Engineering (CS4160) course at TU Delft.
We decided to extend the existing OfflineEuro app by implementing two new features:
two-factor authentication (2FA) and per-transaction ephemeral keys.
These features together aim to improve the overall security of the app.
While 2FA ensures that only legitimate users can access and operate their wallets—protecting against unauthorized access—the use of ephemeral keys complements this by ensuring that, even if access is gained or a key is compromised, individual transactions remain unlinkable and secure.

## 2FA

To enhance user authentication, we integrated a secure two-factor authentication (2FA) mechanism based on QR codes and a trusted third party (TTP). This ensures that only authorized users can operate their wallets by binding a secret key to the user's device during setup and requiring proof of knowledge of that key during each transaction. The implementation consists of two stages: **setup** and **verification**.

### 2FA Setup

![2FA Setup Flow](offlineeuro/images/2fa-setup.jpg)

1. **Create user** – The user initiates account creation in the OfflineEuro app.
2. **Send user info to TTP** – The app transmits basic user information to a trusted third party (TTP).
3. **Open QR scanner** – The app prompts the user to open a scanner interface.
4. **Generate secret at TTP** – The TTP creates a new user entry and associates it with a freshly generated secret key.
5. **Display QR code** – The TTP encodes the secret key into a QR code and displays it.
6. **Scan QR code** – The user scans the QR code and stores the secret key securely within the device. This secret never leaves the device.

### 2FA Verification (Per Transaction)

![2FA Transaction Flow](offlineeuro/images/2fa-transaction.jpg)

1. **Hash construction** – Before initiating a transaction, the device constructs a one-time hash using the stored secret key and current timestamp.
2. **Send hash to TTP** – The hash is sent as a verification request to the TTP.
3. **Hash comparison** – The TTP independently computes the expected hash using its copy of the secret key and compares it with the one sent by the user.
4. **Authorize or deny** – If the hashes match, the TTP responds with a "YES" signal; otherwise, it responds with a "NO".
5. **Transaction execution** – If approved, the transaction proceeds as normal.

This method adds a strong layer of authentication: even if an attacker compromises the interface or steals user credentials, they cannot perform transactions without access to the secret key securely stored on the device.

### Assumptions

- **Trusted TTP**: The system assumes that the trusted third party (TTP) behaves honestly and securely stores the user's secret keys.
- **Secure QR Scanning**: The QR code scanning step during setup is assumed to be secure. Users must ensure that this interaction takes place in a safe environment (e.g., not while screen-sharing or being monitored), as the QR code contains sensitive information that could compromise their wallet if intercepted.
- **Device Integrity**: It is also assumed that the user's device is uncompromised and capable of securely storing the secret key without leakage.

## Ephemeral Keys

The old version of the OfflineEuro app generated a key pair for each user, which was then used to sign every transaction.
This made the transactions linkable, but more importantly if an attacker would gain access to the private key, they can fully impersonate the user and it will compromise all past and future transactions.

Our solution is to generate a new key pair for each transaction, thus ensuring that even if an attacker gains access to the private key, they can only compromise a single transaction.
To retain the auditability and accountability at deposit time, we use a Schnorr signature on the ephemeral public key, which is then included in the token.
This way, the user proves that they generated the transaction and can be held accountable at deposit time, while ensuring that each transaction is unlinkable and secure.
The flow of the application is updated as follows:

![flow](offlineeuro/images/flow.png)

So in addition to all of the other steps when receiving a euro the user should generate a new ephemeral key pair, sign the ephemeral public key with their long-term private key, and include the signature in the token.

Then the user (or bank) that receives the euro token should (in addition to the other steps) verify the signature on the ephemeral public key with the sender's long-term public key and check that the ephemeral key used in the GS transaction proof matches with the one signed.

This is a simplified example diagram of a scenario where user A withdraws a token sends it to user B and that user deposits the euro with the bank verifying with the TTP.
The added differences are indicated in green.

![scenario](offlineeuro/images/scenario.png)

This ephemeral key functionality is integrated inside of the inner workings of the application, without a change on the front-end.
To use it the user just uses the application as normal, with the added benefit that their transactions being unlinkable and secure.