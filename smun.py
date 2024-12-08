size = 0;
#for i in range(0,537474):
for i in range(0,1051691):
    size = size + len(str(i+1))
    #print(i,end='-')

print(str(size))
