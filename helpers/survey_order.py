# Chooses order to test
# Ask about size on open trial

import itertools, random

li = ["0.7OP", "1.0OP", "1.3OP", "0.7BL", "1.0BL", "1.3BL"]

choiceLi = list(itertools.permutations(li, len(li)))

while True:
    print(choiceLi[random.randint(0, len(choiceLi)-1)])
    input()
