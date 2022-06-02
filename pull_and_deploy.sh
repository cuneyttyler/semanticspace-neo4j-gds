cd build/distributions
sudo cp semantic-space-gds-2.0.4.jar /var/lib/neo4j/plugins
sudo systemctl restart neo4j.service
tail -f /var/log/neo4j/debug.log
