/**
 * An interface used by anything that wants to be notified when a NotificationThread ends
 */
interface ThreadCompletionListener
{
    /**
     * This method will be called when a NotificationThread ends executing
     * @param thread : the Thread ( NotificationThread ) which is ending
     */
    void threadCompletedNotification ( Thread thread );
}
