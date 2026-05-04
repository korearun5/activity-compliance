# API Standards

## REST Shape

Use `/api/v1` as the backend API root.

Recommended resources:

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/auth/me`
- `GET /api/v1/users`
- `GET /api/v1/workflows`
- `POST /api/v1/activities`
- `GET /api/v1/activities/{activityId}`
- `POST /api/v1/activities/{activityId}/tasks/{taskId}/evidence`
- `GET /api/v1/reports/summary`
- `POST /api/v1/reports/export`

## Response Envelope

Success:

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "..."
  }
}
```

Failure:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "details": {
      "field": ["reason"]
    },
    "traceId": "..."
  }
}
```

## Standards

- Use UUIDs for public identifiers.
- Include `tenantCode` on login where the client can provide it. Default local tenant is `default`.
- Use ISO-8601 timestamps in API payloads.
- Never expose stack traces in API responses.
- Return validation errors in a stable `details` map.
- Use role checks at controller/service boundaries.
- Log with request id, user id, tenant id, and activity id where available.
- Keep upload APIs multipart, but persist metadata and audit events transactionally.
