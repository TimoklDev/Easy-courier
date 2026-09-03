# Easy Courier

Easy Courier is a RuneLite Sailing assistant for the four common courier training routes:

- The Summer Shore
- Rellekka
- Prifddinas
- Lunar Isle

It does not click, accept, collect, sail, or deliver anything for you. It reads the game state and shows the next useful choice.

## What it does

Easy Courier splits each lap into two clear phases.

### 1. Collection phase

1. Select a route.
2. Press **Start collection**.
3. Follow the current instruction to the next port.
4. Open the notice board.
5. Green tasks are the best choices. Amber tasks are useful alternatives.
6. Blue LATER tasks are valid short hops that are better left until the delivery phase.
7. Accepted tasks use only the game's ACCEPTED stamp and no longer receive another plugin highlight.
8. Tasks above your Sailing level, bounty tasks, backward tasks, and tasks that would consume a reserved slot are dimmed.
9. Pick the tasks you want and close the board.
10. The assistant moves to the next collection stop.

The Prifddinas and Rellekka routes can reserve one open slot for a preferred Aldarin task when it has not appeared yet.

### 2. Delivery phase

1. Press **Move to delivery phase** when the collection lap is ready.
2. Easy Courier builds one route from every active courier task.
3. Cargo pickups always happen before their matching deliveries.
4. Each collect and deliver action remains visible, even when tasks share a dock.
5. The route always finishes at the selected route point.
6. The actual sea lane is visible only while the current step is to sail, both in the game view and on the world map.
7. At a delivery stop, an open notice board only highlights tasks that start at the current port and continue forward.
8. Closing that board completes its checklist step, even when no suitable task was available.

The side panel shows the current instruction, task progress, XP per task, total route XP, and the complete ordered checklist. Every phase also has a **Skip current step** button.

If the plugin starts while courier tasks are already active, it detects the best matching training route and resumes the delivery phase from the live cargo and location state. While sailing, it remembers the last dock so a temporary unknown location cannot restart the route from an earlier port.

## Highlight guide

- Green border: best task available
- Amber border: useful task that fits the route
- Blue border with LATER: valid one-port-forward task that is better taken during delivery
- Darkened card: unavailable, bounty, backward, off-route, or intentionally left for a reserved slot
- Teal dock highlight: collect or deliver cargo here
- Teal cargo outline: an inventory or cargo-hold item used by an active courier task
- Gold notice-board highlight: check this board while a task slot is free
- Teal Trader Crewmember highlight: use the charter required by the current collection step

## Cargo messages

The plugin sends these game messages after the matching state change:

- `You now have all the cargo`
- `You delivered all cargo for this dock`

Several tasks for the same dock are counted together before the message is sent.

## Requirements

- Sailing route and quest requirements still apply in game

The route card shows important unlock notes such as Song of the Elves for Prifddinas, Troubled Tortugans for The Summer Shore, and the level 65 Etceteria option.

## Credits

The public [Port Tasks plugin](https://github.com/nucleon/port-tasks) was used as a behavior and game-data reference. Its BSD license is reproduced in `THIRD_PARTY_NOTICES.txt` inside the plugin JAR. Easy Courier has its own route model, optimizer, state handling, overlays, and interface.
