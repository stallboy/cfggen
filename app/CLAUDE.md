We keep all important docs in .agent folder and keep updating them, structure like below

.agent
- Tasks: PRD & implementation plan for each feature
- System: Document the current state of the system (project structure, tech stack, integration points, database schema, and core functionalities such as agent architecture, LLM layer, etc.)
- SOP: Best practices of execute certain tasks (e.g. how to add a schema migration, how to add a new page route, etc.)
- README.md: an index of all the documentations we have so people know what & where to look for things we should always update .agent docs after we implement certain featrue, to make sure it fully reflect the up to date information

Before you plan any implementation, always read the .agent/README first to get context