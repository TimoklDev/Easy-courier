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
2. Follow the current instruction to the next port.
3. Open the notice board.
4. Green tasks are the best choices. Amber tasks are useful alternatives.
5. Blue LATER tasks are valid short hops that are better left until the delivery phase.
6. Accepted tasks use only the game's ACCEPTED stamp and no longer receive another plugin highlight.
7. Tasks above your Sailing level, bounty tasks, backward tasks, and tasks that would consume a reserved slot are dimmed.
8. Pick the tasks you want and close the board.
9. The assistant moves to the next collection stop.

When the task list becomes full, the remaining collection stops are skipped. Easy Courier chooses the available shipwright closest to the first location in the optimized delivery route. The shipwright is highlighted, along with the first cargo ledger when both are at the same port. Taking that cargo or boarding the recovered boat starts the delivery phase automatically.

The Rellekka to Etceteria step uses the sailing route when its shortcut requirements are not met. With 65 Sailing, 55 Agility, and The Fremennik Trials completed, Easy Courier instead highlights the Rellekka sailor, the Miscellania stepping stone, and a short land route to the Etceteria notice board. The Rellekka and Prifddinas checklists also remind you to recover your boat to Aldarin before starting delivery.

Only the Prifddinas route keeps one slot reserved for Aldarin to Prifddinas until that task is accepted. When only that slot remains, the collection checklist skips directly to Aldarin.

### 2. Delivery phase

1. Press **Move to delivery phase** when the collection lap is ready.
2. Easy Courier builds one route from every active courier task.
3. Cargo pickups always happen before their matching deliveries.
4. Each collect and deliver action remains visible, even when tasks share a dock.
5. The route always finishes at the selected route point.
6. The actual sea lane is visible only while the current step is to sail, both in the game view and on the world map.
7. Entering the active destination's 20-tile docking radius advances to the next action. Other nearby ports are ignored while sailing.
8. Later delivery stops can include a notice-board check, but the route's starting port never repeats that task.
9. Closing that board completes its checklist step, even when no suitable task was available.

The side panel shows the current instruction, task progress, XP per task, total route XP, and the complete ordered checklist. Every phase also has a **Skip current step** button.

After the delivery route reaches its finish and every task is complete, Easy Courier starts the next collection lap automatically. The collection phase also starts automatically when the plugin opens without active courier tasks.

The optional **Show info panel** setting adds a compact top-left display with the current step, route XP, Sailing XP gained this plugin session, and XP per hour. The XP baseline resets with the client session, and the timer begins when the first Sailing XP is gained.

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
- Teal shipwright highlight: recover the boat for the delivery route
- Red 17 by 17 DODGE square: keep clear of a Sailing portal's danger range

The portal warning can be disabled with **Paint portal range for dodging**. It is enabled by default.

## Cargo messages

The plugin sends these game messages with a strong blue **Easy Courier:** label after the matching state change:

- `You now have all the cargo`
- `You delivered all cargo for this dock`

Several tasks for the same dock are counted together before the message is sent.

## Requirements

- Sailing route and quest requirements still apply in game

The route card shows important unlock notes such as Song of the Elves for Prifddinas, Troubled Tortugans for The Summer Shore, and the level 65 Etceteria option.
