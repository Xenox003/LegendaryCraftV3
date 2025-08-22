package de.jxdev.legendarycraft.v3.db.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Team model mapped to the `teams` table.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Team {
    private Integer id; // null if not persisted yet
    private String name;
    private String prefix;
    private String color;

    public Team(String name, String prefix, String color) {
        this(null, name, prefix, color);
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
