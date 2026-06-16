#!/bin/bash

# Distributed Wallet - Phase 10 Verification Script

BASE_URL="http://localhost:8082/wallet"

echo "1. Creating a new wallet for User 101..."
curl -X POST "$BASE_URL/create" -H "Content-Type: application/json" -d '{"userId": 101}'
echo -e "\n"

echo "2. Depositing 1000..."
curl -X POST "$BASE_URL/deposit" -H "Content-Type: application/json" -d '{"walletId": 1, "amount": 1000}'
echo -e "\n"

echo "3. Checking Balance (Materialized View)..."
curl -G "$BASE_URL/balance" --data-urlencode "walletId=1"
echo -e "\n"

echo "4. Performing 9 more deposits to trigger Snapshot (Total 10 events)..."
for i in {1..9}
do
   curl -s -X POST "$BASE_URL/deposit" -H "Content-Type: application/json" -d '{"walletId": 1, "amount": 10}' > /dev/null
done
echo "Deposits completed."

echo "5. Verifying Balance after 10 events..."
curl -G "$BASE_URL/balance" --data-urlencode "walletId=1"
echo -e "\n"

echo "6. Auditing/Rebuilding Balance from Events & Snapshots..."
curl -G "$BASE_URL/audit/rebuild" --data-urlencode "walletId=1"
echo -e "\n"

echo "Verification complete!"
