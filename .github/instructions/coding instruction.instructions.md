---
applyTo: all
rules:
  - language: Java 21
  - artifact: single-jar
  - framework: do not use Spring
  - dependencies: use Lombok annotations
  - comments: write all comments, messages, and javadoc in English
  - code: write code in one plain artifact
  - style: follow clean code principles
  - structure: do not split into modules
  - prefer: explicit types, no var
  - prefer: immutable data where possible
  - prefer: static factory methods over constructors
  - avoid: magic numbers and hardcoded strings
  - avoid: unnecessary abstractions
  - when executing Git commands, always explain what each command does

  