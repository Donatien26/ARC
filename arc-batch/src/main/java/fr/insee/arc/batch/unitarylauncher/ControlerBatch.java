package fr.insee.arc.batch.unitarylauncher;

import fr.insee.arc.core.factory.ApiServiceFactory;
import fr.insee.arc.core.model.TraitementPhase;
//
import fr.insee.arc.utils.batch.Batch;

public class ControlerBatch extends Batch {

    public ControlerBatch(String... someArgs) {
        super(someArgs);
    }

    /**
     *
     * @param args
     *            {@code args[0]} : environnement de travail de départ<br/>
     *            {@code args[1]} : environnement de travail d'arrivée<br/>
     *            {@code args[2]} : répertoire racine<br/>
     *            {@code args[3]} : nombre de lignes maximal à traiter
     */
    public static void main(String[] args) {
        Batch batch = new ControlerBatch(args);
        batch.execute();

    }

    @Override
    public void execute() {
        ApiServiceFactory.getService(TraitementPhase.CONTROLE.toString(), (String) this.args[0], (String) this.args[1],
                (String) this.args[2], (String) this.args[3], (String) this.args[4]).invokeApi();

    }

}
