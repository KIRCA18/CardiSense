package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "ml_models")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MlModel {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String modelKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String checkpoint;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String inputAdapter;

    @Column(length = 1000)
    private String description;

    private String device;

    @Column(nullable = false)
    private boolean active = true;

    public MlModel(String modelKey, String name, String checkpoint, String version, String inputAdapter) {
        this.modelKey = modelKey;
        this.name = name;
        this.checkpoint = checkpoint;
        this.version = version;
        this.inputAdapter = inputAdapter;
    }

}
