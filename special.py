import math
size = 0;
limit = 9846720;
#limit = 4923360;
#limit = 2461680;
#limit = 1230840;
i=0;
char = [chr(0x7e),chr(0x21)]
color = char[0]
section = limit/((1560/20)/3)
while size < limit:
    color = char[((math.floor(i/section)) % 2)*(math.floor(i/(2104/10))%2)]
    size = size + 1
    print(color,end='')
    i = i + 1
