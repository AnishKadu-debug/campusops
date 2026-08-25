# CampusOps — Live Demo Script

Ordered, copy-pasteable walkthrough of the full stack. All commands are PowerShell and were executed against the real repository state.

## 0. Start the environment

Start Docker Desktop, then from the repository root:

```powershell
docker compose up -d --build
docker compose ps        # wait until all 7 services show (healthy)
```

Quick sanity checks:

```powershell
Invoke-RestMethod http://localhost:8082/actuator/health   # UP
Invoke-RestMethod http://localhost:8083/actuator/health   # UP
Invoke-RestMethod http://localhost:8084/actuator/health   # UP
```

## 1. Generate development tokens

There is no login system — tokens are minted locally by `DevTokenGenerator`:

```powershell
cd incident-service
.\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target\cp.txt"
$cp = "target\classes;$((Get-Content target\cp.txt -Raw).Trim())"

$student = & java -cp $cp com.campusops.incident.security.dev.DevTokenGenerator STUDENT student-A
$tech    = & java -cp $cp com.campusops.incident.security.dev.DevTokenGenerator TECHNICIAN tech-A
$manager = & java -cp $cp com.campusops.incident.security.dev.DevTokenGenerator MANAGER manager-1
cd ..
```

Tokens live 8 hours; re-run if you get a 401 mid-demo.

## 2. Authentication gate — no JWT means 401

```powershell
try { Invoke-RestMethod http://localhost:8082/api/v1/incidents } catch { [int]$_.Exception.Response.StatusCode }   # 401
```

## 3. Student creates an incident — identity is forced from the JWT

The body claims a different reporter on purpose; the service must ignore it:

```powershell
$h = @{ Authorization = "Bearer $student" }
$body = '{"title":"Projector dead in Lab 7","description":"Demo step 3","priority":"MEDIUM","reporterId":"hacker-should-be-ignored"}'
$inc = Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/v1/incidents -Headers $h -ContentType "application/json" -Body $body
$inc.reporterId       # -> student-A   (JWT subject won, body ignored)
$inc.slaDeadline      # -> created_at + 12h (MEDIUM)
$id = $inc.id
```

## 4. Manager assigns a technician (MANAGER-only endpoint)

```powershell
$mh = @{ Authorization = "Bearer $manager" }
$inc = Invoke-RestMethod -Method Patch -Uri "http://localhost:8082/api/v1/incidents/$id/assign" `
        -Headers $mh -ContentType "application/json" -Body '{"assigneeId":"tech-A"}'
$inc.status; $inc.assigneeId          # -> ASSIGNED / tech-A
```

## 5. Technician works their queue

```powershell
$th = @{ Authorization = "Bearer $tech" }
# assigned incident is visible to its technician...
Invoke-RestMethod -Uri "http://localhost:8082/api/v1/incidents/$id" -Headers $th
# ...and the technician can progress it through the state machine
Invoke-RestMethod -Method Patch -Uri "http://localhost:8082/api/v1/incidents/$id/status" `
    -Headers $th -ContentType "application/json" -Body '{"status":"IN_PROGRESS"}'
```

## 6. Technicians can never assign

```powershell
try {
  Invoke-RestMethod -Method Patch -Uri "http://localhost:8082/api/v1/incidents/$id/assign" `
    -Headers $th -ContentType "application/json" -Body '{"assigneeId":"tech-A"}'
} catch { [int]$_.Exception.Response.StatusCode }     # 403
```

Students get 403 on the same call.

## 7. Peek at the Kafka event

Host tooling still uses `localhost:9092`; containers use `kafka:9092`. Easiest in-demo view:

```powershell
docker exec campusops-kafka /opt/kafka/bin/kafka-console-consumer.sh `
  --bootstrap-server kafka:9092 --topic incident.created.v1 `
  --from-beginning --max-messages 1 --timeout-ms 5000
```

## 8. Notification appeared via Kafka

```powershell
Invoke-RestMethod -Uri "http://localhost:8084/api/v1/notifications?incidentId=$id"
# INCIDENT_CREATED row for the reporter, plus INCIDENT_ASSIGNED row for tech-A after step 4
```

## 9. Asset flow — Eureka discovery + Feign JWT propagation

Create an asset, then an incident referencing it; the incident's asset validation travels Incident → Eureka → Asset Service carrying the *student's* JWT:

```powershell
$asset = Invoke-RestMethod -Method Post -Uri http://localhost:8083/api/v1/assets `
           -Headers $mh -ContentType "application/json" `
           -Body '{"name":"Projector Lab 7","type":"PROJECTOR","location":"Lab 7"}'
$body = '{"title":"Same projector again","description":"Feign path demo","priority":"LOW","assetId":"' + $asset.id + '"}'
$inc2 = Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/v1/incidents -Headers $h -ContentType "application/json" -Body $body
$inc2.assetId         # resolved successfully -> discovery + propagation work
```

If Eureka or JWT propagation were broken you would see the circuit-breaker fallback asset name instead.

## 10. SLA breach demo — force a fast breach

Recreate only Incident Service with a one-minute CRITICAL window:

```powershell
$env:CAMPUSOPS_SLA_DURATION_CRITICAL = "PT1M"
docker compose up -d --force-recreate incident-service
Remove-Item Env:\CAMPUSOPS_SLA_DURATION_CRITICAL
# wait for health to return to UP (~20s), then:
$ch = @{ Authorization = "Bearer $student" }
$crit = Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/v1/incidents `
          -Headers $ch -ContentType "application/json" `
          -Body '{"title":"Elevator stuck","description":"SLA demo","priority":"CRITICAL"}'
"deadline: $($crit.slaDeadline)"
```

Wait ~100 seconds (60s deadline + up to 30s scan interval), then:

```powershell
Invoke-RestMethod -Uri "http://localhost:8084/api/v1/notifications?incidentId=$($crit.id)"
# -> SLA_BREACHED row for the reporter, exactly once (deduplicated by slaBreachedAt)
```

Restore the default window afterwards:

```powershell
docker compose up -d --force-recreate incident-service
```

## Cleanup

```powershell
docker compose down       # normal cleanup - keeps database volumes
```

`docker compose down -v` additionally deletes the named volumes (all containerized data) — use only when you intend a full reset.

---

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `ports are not available` when starting Compose | Leftover native dev servers hold 8082–8084/8761 (`Get-NetTCPConnection -State Listen | Where LocalPort -in 8082,8083,8084,8761`), or native Postgres/Mongo occupy 5433/27018 (they shouldn't — those were chosen to avoid your native instances). Stop the stale process or change the mapping. |
| Service container restart-looping | Check `docker logs campusops-<service>`; most common cause is infrastructure not yet healthy — Compose gates on `service_healthy`, so a failing postgres/kafka/mongo/eureka healthcheck delays everything downstream. |
| Sudden 401 on a previously working call | The 8-hour token expired — mint a fresh one (step 1). |
| Incident creation returns fallback asset name `"UNAVAILABLE (Circuit Breaker Fallback)"` | Asset Service was down/tripped the breaker at validation time; check `docker compose ps campusops-asset` and its logs. |
| Eureka connection refused to `localhost:8761` in logs | The env var must be `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` — there is no `SPRING_` prefix on this property family. Verify with `(docker exec campusops-incident env) \| Select-String EUREKA`. |
| No notification rows appearing | Confirm `campusops-notification` is healthy and events exist on the topic (step 7). Remember `auto-offset-reset=earliest` applies only to new consumer groups. |
| SLA demo never breaches | You forgot the `PT1M` recreate (step 10) or waited less than deadline + scan interval; verify with `(docker exec campusops-incident env) \| Select-String SLA`. |
