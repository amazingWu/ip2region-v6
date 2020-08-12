# db maker java implementation

### 1, How to build ?
```
maven package -Dmaven.test.skip=true
```


### 2, How to make ?
```
java -jar ip2region-maker-{version}-with-dependencies.jar -s {must: path of ip.merge.txt} -t {must: file name of db} -t {must: ipv6 | ipv4} -l {option: total header block size. default value is 20 * 2048}.
```
