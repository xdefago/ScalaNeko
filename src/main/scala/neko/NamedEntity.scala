package neko

/**
 * Created by defago on 02/05/2017.
 */
trait NamedEntity
{
  def name: String
  
  def simpleName: String = name
  
  def senderOpt: Option[Sender]
  
  def context: Option[PID] = None
}
