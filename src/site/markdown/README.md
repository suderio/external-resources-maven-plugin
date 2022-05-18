## Read me first

### Usage

```xml
                <plugins>
                    ...                
                    <plugin>
                        <groupId>net.technearts</groupId>
                        <artifactId>external-resources-maven-plugin</artifactId>
                        <version>0.1.0</version>
                        <executions>
                            <execution>
                                <id>git</id>
                                <phase>initialize</phase>
                                <goals>
                                    <goal>git</goal>
                                </goals>
                                <configuration>
                                    <source>https://github.com/xxxx/something.git</source>
                                    <branch>master</branch>
                                    <target>${project.build.directory}/git</target>
                                    <server>github</server>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                    ...
                </plugins>
```