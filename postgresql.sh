sudo systemctl start postgresql
sudo -u postgres psql


psql -h localhost -U dev1 -d stock -W