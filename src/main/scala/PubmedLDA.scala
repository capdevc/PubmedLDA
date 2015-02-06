// Import the required Mallet code.                                             
import cc.mallet.types.{Instance, InstanceList}
import cc.mallet.pipe.{Pipe, SerialPipes}
import cc.mallet.pipe.{CharSequenceLowercase, CharSequence2TokenSequence, TokenSequence2FeatureSequence}
import cc.mallet.pipe.iterator.LineIterator
import cc.mallet.topics.ParallelTopicModel  

import java.io.File
import java.util.regex.Pattern


object PubmedLDA {

  def main(args:Array[String]) {

    val tok_pattern = Pattern.compile("[a-z]+")
    val pipes = new SerialPipes((Array(new CharSequence2TokenSequence(tok_pattern),
                                       new TokenSequence2FeatureSequence())))

    val lit = new LineIterator(args(0), "(\\S+)\\t(.+)", 2, 0, 1)

    val inst_list = new InstanceList(pipes)

    inst_list.addThruPipe(lit)

    val model = new ParallelTopicModel(100, 50, 0.01)
    model.addInstances(inst_list)
    model.setTopicDisplay(50, 10)
    model.setNumIterations(500)
    model.setOptimizeInterval(25)
    model.setBurninPeriod(200)
    model.setSymmetricAlpha(false)
    val threads = Runtime.getRuntime.availableProcessors
    model.setNumIterations(threads)
    model.estimate


    model.getTopWords(10).map(x => println(x.mkString(" ")))
    inst_list.save(new File(args(1)))
  }

}
