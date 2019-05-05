import subprocess
import random
import math


port_create = 7000
dim_finger_table = 5
cip = "127.0.0.1"
cp = 1717
jip = "127.0.0.1"
jp = 7000


dim = (int)(math.pow(2, dim_finger_table))
port_list = []


for i in range(1,dim):
    port_list.append(port_create + i)

random.shuffle(port_list)


for i in range(len(port_list)):
    name = input("New Node? ")
    if name == "n":
        exit(0)
    port = port_list.pop()
    cmd = "/Users/edoardo/Desktop/prova.sh " + "-p " + str(port) + " -a " + str(cip) + " -b " \
          + str(cp) + " -c " + str(jip) + " -d " + str(jp)
    print(cmd)
    subprocess.call(cmd, shell=True)


print("END")

