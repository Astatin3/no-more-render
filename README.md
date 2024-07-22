# no-more-render

A Fabric mod that removes Minecraft's window rendering code, to create a headless CLI version

## Usage
This mod runs a tcp server locally that allows for a command line interface   
The mod runs on the port 65000, and increments this value by 1 if it cannot access that port.  
You can connect to it via running `ncat localhost 65000` or other similar command
### Comamnds:
```
listelements/elems                     - List widgets on the screen
clickelement/celem <elem index>        - Click an element on the screen
writeelement/welem <elem index> <text> - Write text into a compatable element

key <key>                              - Press and release a key
keydown <key>                          - Press a key
keyup <key>                            - Release a key

connect <Addr>:[Port]                  - Forcibly connect to a server

quit/exit                              - Close the game
```
