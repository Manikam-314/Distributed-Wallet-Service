import mysql.connector
try:
    conn = mysql.connector.connect(
        host='localhost',
        port=3307,
        user='root',
        password='rootroot1234',
        database='wallet_db'
    )
    cursor = conn.cursor()
    cursor.execute("SELECT id, user_id, balance FROM wallets WHERE id = 27")
    row = cursor.fetchone()
    print(f"HOST_TEST_RESULT: {row}")
    conn.close()
except Exception as e:
    print(f"HOST_TEST_ERROR: {e}")
