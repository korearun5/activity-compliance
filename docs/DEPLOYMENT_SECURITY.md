# Production Deployment Guide

## Security Hardening Checklist

### 1. SSL/TLS Configuration

**Option A: Application-Level SSL (Embedded Tomcat)**

The `prod` profile supports embedded SSL through `APP_SSL_*` variables. Leave
`APP_SSL_ENABLED=false` when TLS is terminated by a reverse proxy or load
balancer.

Create `.env` file or set environment variables:
```bash
# For PKCS12 keystore (recommended)
export APP_SSL_ENABLED=true
export APP_SSL_KEYSTORE_PATH=/etc/tomcat/keystore.p12
export APP_SSL_KEYSTORE_PASSWORD=<your-secure-password>
export APP_SSL_KEYSTORE_TYPE=PKCS12
export APP_SSL_KEY_ALIAS=tomcat

# For JKS keystore
export APP_SSL_KEYSTORE_TYPE=JKS
```

Generate self-signed certificate (for testing):
```bash
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 365
```

Production: Use certificate from trusted CA (e.g., Let's Encrypt, AWS ACM)

**Option B: Reverse Proxy SSL (Recommended for Production)**

Use Nginx or AWS Application Load Balancer for SSL termination. This is more secure and scalable.

### 2. Environment Variables (Production)

```bash
# Database
export APP_DB_URL=jdbc:postgresql://db-prod.example.com:5432/activity_platform
export APP_DB_USERNAME=app_user
export APP_DB_PASSWORD=<strong-password>

# JWT
export APP_JWT_ISSUER=activity-compliance-prod
export APP_JWT_SECRET=<generate-strong-256-bit-secret>
export APP_ACCESS_TOKEN_TTL=PT30M
export APP_REFRESH_TOKEN_TTL=P7D

# CORS
export APP_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
export APP_CORS_ALLOW_CREDENTIALS=true

# Storage
export APP_STORAGE_PROVIDER=minio
export APP_MAX_UPLOAD_BYTES=10485760
export APP_MINIO_ENDPOINT=https://minio.example.com
export APP_MINIO_BUCKET=activity-platform
export APP_MINIO_ACCESS_KEY=<minio-access-key>
export APP_MINIO_SECRET_KEY=<minio-secret-key>
export APP_MINIO_REGION=us-east-1
export APP_MINIO_SECURE=true
export APP_MINIO_CREATE_BUCKET_IF_MISSING=false

# Server
export APP_PORT=8080
export APP_SSL_ENABLED=true
export APP_SSL_KEYSTORE_PATH=/etc/app/keystore.p12
export APP_SSL_KEYSTORE_PASSWORD=<keystore-password>

# Logging
export APP_LOG_LEVEL_ROOT=WARN
export APP_LOG_LEVEL_APP=INFO
export APP_LOG_LEVEL_SECURITY=WARN

# OpenAPI / Swagger
export APP_OPENAPI_ENABLED=false
export APP_SWAGGER_UI_ENABLED=false

# Features
export APP_SEED_ENABLED=false

# Frontend build/runtime
export EXPO_PUBLIC_API_BASE_URL=https://api.example.com
export EXPO_PUBLIC_API_VERSION=v1
export EXPO_PUBLIC_DEFAULT_TENANT_CODE=default
```

The `prod` profile validates these settings at startup and fails fast when:

- `APP_DB_URL`, `APP_DB_USERNAME`, or `APP_DB_PASSWORD` is missing
- `APP_DB_URL` points to `localhost` or `127.0.0.1`
- `APP_JWT_SECRET` is shorter than 48 characters or looks like a placeholder
- `APP_CORS_ALLOWED_ORIGINS` is empty, wildcard, or not HTTPS
- `APP_STORAGE_PROVIDER` is not `minio`
- required MinIO endpoint, bucket, access key, or secret key is missing
- `APP_MINIO_SECURE=true` is paired with an `http://` MinIO endpoint

Use `.env.production.example` as the complete deployment template and store real
secrets in a deployment secret manager.

### 3. Rate Limiting (Optional Edge/Application Control)

Add dependency to `pom.xml`:
```xml
<dependency>
  <groupId>io.github.bucket4j</groupId>
  <artifactId>bucket4j-core</artifactId>
  <version>7.6.0</version>
</dependency>
```

Create rate limiting filter (optional):
```java
@Component
public class RateLimitingFilter extends OncePerRequestFilter {
  private final Bucket bucket = Bucket4j.builder()
    .addLimit(Limit.of(100, Refill.intervally(100, Duration.ofMinutes(1))))
    .build();

  @Override
  protected void doFilterInternal(HttpServletRequest request, 
                                 HttpServletResponse response, 
                                 FilterChain chain) throws ServletException, IOException {
    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
    } else {
      response.setStatus(429);
      response.getWriter().write("Too many requests");
    }
  }
}
```

For production, prefer AWS WAF, Cloudflare, Nginx, or load balancer rate
limiting at the edge. Add an application filter only if the target hosting
environment does not provide this control.

### 4. Security Scanning

**Run OWASP Dependency Check before deployment:**
```bash
cd backend
./mvnw -B -Psecurity-scan -DskipTests verify

# Or standalone:
./mvnw org.owasp:dependency-check-maven:check
```

**View report:**
```bash
# Reports generated at:
# backend/target/dependency-check-report.html
# backend/target/dependency-check-report.json
```

### 5. Running in Production

**Docker Container (Recommended)**
```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY backend/target/backend-*.jar app.jar

# Copy keystore for SSL
COPY keystore.p12 /etc/app/keystore.p12

ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"

EXPOSE 8080

CMD java ${JAVA_OPTS} \
  -Dspring.profiles.active=prod \
  -Dserver.ssl.enabled=true \
  -Dserver.ssl.key-store=/etc/app/keystore.p12 \
  -Dserver.ssl.key-store-password=${APP_SSL_KEYSTORE_PASSWORD} \
  -Dserver.ssl.key-store-type=PKCS12 \
  -jar app.jar
```

**Docker Compose Example**
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - APP_DB_URL=jdbc:postgresql://db:5432/activity_platform
      - APP_DB_USERNAME=app_user
      - APP_DB_PASSWORD=${DB_PASSWORD}
      - APP_JWT_SECRET=${JWT_SECRET}
      - APP_CORS_ALLOWED_ORIGINS=https://app.example.com
      - APP_STORAGE_PROVIDER=minio
      - APP_MINIO_ENDPOINT=http://minio:9000
      - APP_MINIO_BUCKET=activity-platform
      - APP_MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY}
      - APP_MINIO_SECRET_KEY=${MINIO_SECRET_KEY}
      - APP_MINIO_SECURE=false
    depends_on:
      - db
      - minio
    
  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_USER=app_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
      - POSTGRES_DB=activity_platform
    volumes:
      - db-data:/var/lib/postgresql/data

  minio:
    image: ${MINIO_IMAGE}
    command: server /data --console-address ":9001"
    environment:
      - MINIO_ROOT_USER=${MINIO_ACCESS_KEY}
      - MINIO_ROOT_PASSWORD=${MINIO_SECRET_KEY}
    volumes:
      - minio-data:/data

volumes:
  db-data:
  minio-data:
```

Pin `MINIO_IMAGE` to an approved release for your environment instead of using a
floating tag. In internet-facing production, use HTTPS MinIO/S3 endpoints or a
private network plus explicit transport controls.

### 6. Monitoring & Logging

**Enable actuator endpoints for monitoring:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**Monitor logs for security issues:**
```bash
# Example: Monitor failed login attempts
grep "AUTH_LOGIN" app.log | grep error | wc -l
```

### 7. Regular Maintenance

- **Weekly**: Check for new dependency vulnerabilities
  ```bash
  mvn dependency:tree > dependencies.txt
  ```

- **Monthly**: Run full security scan
  ```bash
  mvn clean verify -P security-scan
  ```

- **Quarterly**: Rotate JWT secret (if needed)
- **Annually**: Renew SSL certificates

### 8. Incident Response

**If JWT secret is compromised:**
1. Generate new JWT_SECRET
2. Deploy new version with updated secret
3. Invalidate existing tokens by rotating secret (client must re-authenticate)
4. Review audit logs for suspicious activity

**If database credentials are compromised:**
1. Change `APP_DB_PASSWORD` immediately
2. Check audit logs for unauthorized access
3. Review data access patterns

### 9. Security Headers (Already Configured)

The application now includes security headers:
- X-Content-Type-Options: nosniff
- X-Frame-Options: DENY
- Content-Security-Policy: default-src 'self'
- Strict-Transport-Security: max-age=31536000
- X-XSS-Protection: 1; mode=block

### 10. Compliance & Audit Trail

- All user actions are logged to the audit table.
- Request tracing uses the `X-Request-Id` header.
- Structured logging includes `requestId` in MDC.
- Audit events are stored in the database as append-only records.
- Sensitive values such as passwords and tokens must not be logged.

---

## Deployment Checklist

- [ ] Environment variables configured
- [ ] SSL/TLS certificate obtained and installed
- [ ] Database backup configured
- [ ] OWASP dependency check passed
- [ ] Security headers enabled
- [ ] Rate limiting configured
- [ ] Monitoring & alerting set up
- [ ] Backup & disaster recovery tested
- [ ] Security team reviewed
- [ ] Load testing completed

---

## Support & Documentation

- [Spring Boot Security](https://spring.io/projects/spring-security)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Boot Production Guide](https://spring.io/guides/gs/producing-web-service/)
