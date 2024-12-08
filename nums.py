size = 0;
limit = 9846720;
#limit = 4923360;
#limit = 2461680;
#limit = 1230840;
i=0;
while size < limit:
    size = size + len(str(i+1))
    print(i,end='-')
    i = i + 1
