Warehouse Bots - Sequential

Instruction format:
number_of_bots
A B
B1|(from)shelfX-shelfY-slot|(to)shelfX-shelfY-slot

The parser also accepts the untagged endpoint form shown in the assignment example.
Warehouse dimensions are read dynamically from A B.
Use InstructionGenerator.java to configure bot count, grid dimensions, tasks per bot, and seed.
