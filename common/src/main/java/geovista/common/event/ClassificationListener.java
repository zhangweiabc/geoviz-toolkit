/* Licensed under LGPL v. 2.1 or any later version;
 see GNU LGPL for details.
 Original Author: Frank Hardisty */

package geovista.common.event;

import java.util.EventListener;

/**
 * This interface enables listening to senders of ClassificationEvents.
 * 
 * This interface also enables "fireClassificationChanged" methods in classes
 * that generate and broadcast ClassificationEvents.
 * 
 */
public interface ClassificationListener extends EventListener {

	public void classificationChanged(ClassificationEvent e);

}