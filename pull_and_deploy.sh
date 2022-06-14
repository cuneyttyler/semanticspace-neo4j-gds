git pull origin test
cd build/distributions
sudo cp semantic-space-gds-1.0.1.jar /var/lib/neo4j/plugins
sudo systemctl restart neo4j.service
tail -f /var/log/neo4j/debug.log
