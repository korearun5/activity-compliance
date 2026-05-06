# Project Documentation

This folder contains the working documentation for the reusable
activity-compliance platform.

## Current Documents

- [Architecture Guide](architecture.md): system architecture, module boundaries,
  diagrams, data model, request flow, and reuse rules.
- [Component And Data Model Diagrams](component-and-data-model-diagrams.md):
  component architecture, backend module map, and PostgreSQL class/table
  diagrams.
- [Use Case Guide](use-cases.md): actors, use-case diagram, main flows, role
  permissions, and agriculture scenario.
- [Developer Guide](developer-guide.md): setup, local commands, ports, backend
  and frontend standards, API areas, testing, and implementation workflow.
- [QA Guide](qa-guide.md): automated checks, local smoke tests, API smoke tests,
  and regression checklist.
- [API Standards](api-standards.md): REST shape, response envelope, errors, and
  current endpoint conventions.
- [Database Notes](database.md): schema and database design notes.
- [Deployment Security](DEPLOYMENT_SECURITY.md): deployment and security
  checklist.
- [Deployment Guide](deployment-guide.md): Docker stack, production runtime,
  required environment variables, and release checklist.
- [Security Assessment](SECURITY_ASSESSMENT.md): current security review notes.
- [Client Admin Workflow Guide](client-admin-workflow-guide.md): admin workflow
  notes for the agriculture client.
- [Roles And Supervisors](roles-and-supervisors.md): role meanings, supervisor
  setup, permissions, and daily usage.

## Documentation Status

These documents describe the current development foundation. They are suitable
for internal development, deployment preparation, and planning. The repository
also includes `.env.example` for local Docker defaults and
`.env.production.example` for production environment planning.

Before client production handoff, create a final client-facing documentation set:

- Final operations runbook.
- User/admin manual.
- Full QA/UAT test case catalog.
- Backup and recovery guide.
