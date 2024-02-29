# zenoh RAM protocol specification

Every Zenoh ram must specify a base key to use to create "topics" to publishers/subscribers/queryables for communication.
 - `{base_key}\changes` - publisher that publishes changes to the memory
   - CHANGE EVENT
     - 4 bytes of address
     - 4 bytes of length
     - data values rounded up to nearest power of 2 bytes for data width (obtainable from `info`) - ie 1,2,3...8 bits will use one byte, while 20 bits will use 4 bytes
 - `{base_key}\set` - subscriber that listens for set and updates the RAM with the data provided
   - 4 bytes of address
   - 4 bytes of length
   - data values rounded up to nearest power of 2 bytes for data width (obtainable from `info`)
 - `{base_key}\get` - queryable that returns the data specified
   - 4 bytes of address
   - 4 bytes of length
   - data values
 - `{base_key}\info` - queryable that returns structure of ram
   - 4 bytes of length
   - 4 bytes of data width