## Code Philosophy
As a code agent, follow these core design principles from John Outerhout's "A Philosophy of Software Design" in all your coding tasks:

1. **Manage Complexity as the Primary Goal**: Prioritize reducing complexity in software design. Complexity arises from dependencies, obscurity, and unnecessary code—always strive to eliminate it.
2. **Embrace Deep Modules**: Create modules, classes, or functions that are "deep"—meaning they provide significant functionality through simple interfaces. Avoid shallow modules with complex interfaces but little functionality.
3. **Practice Information Hiding**: Conceal implementation details within modules. Expose only essential interfaces to minimize dependencies and reduce cognitive load for users.
4. **Define Simple Interfaces**: Keep interfaces small, clear, and stable. Avoid leaking implementation details through interfaces.
5. **Promote Modularity**: Design systems with loosely coupled modules that have single, well-defined responsibilities. Minimize interactions between modules to prevent tight coupling.
6. **Avoid Repetition and duplication**: Eliminate code duplication by abstracting common functionality. This reduces errors and simplifies maintenance.
7. **Use Descriptive Naming**: Choose clear, precise names for variables, functions, and classes that reveal intent without needing additional comments.
8. **Handle Errors Simply**: Implement straightforward error handling that doesn't overcomplicate the design. Focus on fail-fast mechanisms and clear error propagation.
9. **Iterate on Design**: Treat design as an iterative process. Refactor code continuously to improve simplicity and reduce complexity, rather than treating initial designs as final.
10. **Favor Composition Over Inheritance**: Where appropriate, use composition to build flexibility and avoid the pitfalls of deep inheritance hierarchies.

In your code, always ask: "Is this the simplest way to achieve the goal? Does it hide unnecessary details? Does it minimize future complexity?" Apply these principles proactively to write clean, maintainable, and efficient software.


## Document
We maintain all essential documentation in the .agent folder, updating it regularly. All documents should be kept concise and focused. The folder structure is as follows:

- Tasks: PRD & implementation plan for each feature
- System: Current system state (project structure, tech stack, integrations, database schema, core functionalities)
- SOP: Best practices for common tasks (e.g., schema migrations, adding new routes). Can be empty.
- ref: Reference materials (API docs, design specs). Read-only — consult but do not modify.
- README.md: Index of all documents for easy navigation

After implementing a feature, update the relevant .agent docs (except ref) to ensure they reflect the latest state. Always prioritize clarity and brevity.

Always read .agent/README.md for context.
