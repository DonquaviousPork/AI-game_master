# AI Game Master (EECS3311 course project)

A single-player, D&D-inspired text RPG where an AI game master generates the
scenario and narrates the story, while deterministic Java code owns the rules,
dice, and game state.

## Prerequisites
- JDK 17 or newer
- Maven 3.9+

## Setup
1. Copy `.env.example` to `.env`.
2. Put your own LLM API key in `.env` (never commit this file).

## Build and test
    mvn test

## Run
GUI and CLI launch commands will be added as those parts are implemented.

## Status
Work in progress. See `CHANGES.md` for differences from the Stage 1 design.
