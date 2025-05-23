package src.game;

interface FullDeployment {
    public DeploymentCard getDeploymentCard();

    public default void setDeploymentVisibility(boolean value) {
        getDeploymentCard().setVisible(value);
    }
}
