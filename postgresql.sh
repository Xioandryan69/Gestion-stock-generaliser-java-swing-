sudo systemctl start postgresql
sudo -u postgres psql


psql -h localhost -U dev1 -d stock -W
pg_dump -U dev1 -h localhost -d stock > export_db.sql

