package de.jxdev.legendarycraft.v3.team;

import java.util.Objects;

/**
 * Team model mapped to the `teams` table.
 */
public class Team {
    private Integer id; // null if not persisted yet
    private String name;
    private String prefix;
    private String color;

    public Team() {}

    public Team(Integer id, String name, String prefix, String color) {
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.color = color;
    }

    public Team(String name, String prefix, String color) {
        this(null, name, prefix, color);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        // If both have IDs, compare by ID. Else fallback to name.
        if (this.id != null && team.id != null) {
            return Objects.equals(this.id, team.id);
        }
        return Objects.equals(this.name, team.name);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", prefix='" + prefix + '\'' +
                ", color='" + color + '\'' +
                '}';
    }
}
