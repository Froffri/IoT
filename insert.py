#!/usr/bin/env python3
import random
import mysql.connector

# Generate random data and write to a csv file
BASETIME = 1689246000

# Connect to the database and insert the data from the csv file
db = mysql.connector.connect(
    host="localhost",
    user="ops",
    password="opsops",
    database="ioT_Project"
)

cursor = db.cursor()
sql = "INSERT INTO `consumed power` (subzone, value, timestamp) VALUES (%s, %s, FROM_UNIXTIME(%s))"

for i in range(1, 1000001):
    id = random.randint(1, 10)
    value = random.randint(20000, 150000)
    timestamp = BASETIME + i * 10
    cursor.execute(sql, [str(id), str(value), str(timestamp)])

db.commit()
cursor.close()
db.close()
