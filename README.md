# Betting Service

## Build & Run

```bash
# Build jar files
./build.sh

# Run 
java -jar betting-server.jar
```

The server starts on port **8001**.

## API Examples

```bash
# 1. Get/create session
curl http://localhost:8001/1/session

# 2. Post a stake (replace <key> with session key)
curl -X POST "http://localhost:8080/1/stake?sessionkey=<key>" -d "1000"

# 3. Get high stakes list
curl http://localhost:8001/1/highstakes
```
