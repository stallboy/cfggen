We maintain all essential documentation in the .agent folder, updating it regularly. All documents should be kept concise and focused. The folder structure is as follows:

.agent
- Tasks: PRD & implementation plan for each feature
- System: Current system state (project structure, tech stack, integrations, database schema, core functionalities)
- SOP: Best practices for common tasks (e.g., schema migrations, adding new routes)
- ref: Reference materials (API docs, design specs). Read-only — consult but do not modify.
- README.md: Index of all documents for easy navigation

After implementing a feature, update the relevant .agent docs to ensure they reflect the latest state. Always prioritize clarity and brevity.
Before starting any implementation, first read .agent/README.md for context.