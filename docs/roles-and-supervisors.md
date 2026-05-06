# Roles And Supervisors

The platform has three core roles:

- `ADMIN`: full tenant administrator.
- `SUPERVISOR`: operational manager who can run field workflow operations but
  cannot control high-risk account permissions.
- `PARTICIPANT`: field user who performs assigned activities and uploads proof.

## What A Supervisor Is

A supervisor is usually a field manager, project coordinator, district officer,
or operations lead. They help run the workflow after the admin has configured
the tenant.

Supervisors can:

- View participant and activity data for the tenant.
- Create and update workflow definitions.
- Start or assign activities for participants.
- Review proof/evidence submissions.
- View report summaries and export reports.
- Queue and update notification status records.
- View role assignments.

Supervisors cannot:

- Change user roles.
- Remove their own guardrails by assigning themselves admin permissions.
- Bypass tenant boundaries.

Only admins can change roles.

## How To Add A Supervisor

Recommended app flow:

1. Login as an admin.
2. Open the Admin dashboard.
3. Create the person as a normal participant/user from the `Participants` tab.
4. Open the `Roles` tab.
5. Find that user.
6. Select `Supervisor`.
7. Remove `Participant` if the person should only manage work and not submit
   their own proof.

The user should sign out and sign in again after roles change so the JWT token
contains the latest role set.

## API Flow

Create the user:

```http
POST /api/v1/users
```

Then update roles:

```http
PUT /api/v1/users/{userId}/roles
```

Supervisor-only request body:

```json
{
  "roles": ["SUPERVISOR"]
}
```

User who can supervise and also submit their own participant work:

```json
{
  "roles": ["SUPERVISOR", "PARTICIPANT"]
}
```

Tenant admin:

```json
{
  "roles": ["ADMIN"]
}
```

## How To Use Supervisors

Use supervisors when the admin should not be doing day-to-day operations.

Typical usage:

- Admin sets up the tenant, production settings, first users, and role policy.
- Supervisor creates or updates workflow definitions for the local process.
- Supervisor assigns activities to participants.
- Participant completes tasks and uploads evidence.
- Supervisor reviews proof and queues follow-up notifications.
- Admin periodically reviews reports, exports, and audit records.

For small deployments, the admin can do supervisor work too. For production or
multi-region deployments, keep admin access limited and let supervisors manage
daily workflow operations.
