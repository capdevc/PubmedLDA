// Import the required Mallet code.                                             
import cc.mallet.types.{Instance, InstanceList}
import cc.mallet.pipe.{Pipe, SerialPipes}
import cc.mallet.pipe.{CharSequenceLowercase, CharSequence2TokenSequence, TokenSequence2FeatureSequence}
import cc.mallet.pipe.iterator.LineIterator
import cc.mallet.topics.ParallelTopicModel  
import scopt.OptionParser

import collection.JavaConversions._
import java.io.{BufferedReader, FileReader, File}
import java.util.regex.Pattern


object PubmedLDA {
  class MyLI(input:File, lineRegex:Pattern, dataGroup:Int, targetGroup:Int, uriGroup:Int) extends Iterator[Instance] {
    import java.io.LineNumberReader
    val reader = new LineNumberReader(new FileReader(input))
    var currentLine = ""
    currentLine = reader.readLine()

    def next()  = {
      var uriStr:String = null
      var data:String = null
      var target:String = null
      val matcher = lineRegex.matcher(currentLine)
      if (matcher.find()) {
        if (uriGroup > -1)
          uriStr = matcher.group(uriGroup)
        if (targetGroup > -1)
          target = matcher.group(targetGroup)
        if (dataGroup > -1)
          data = matcher.group(dataGroup)
      } else
        throw new IllegalStateException ("Line #"+reader.getLineNumber()+" does not match regex");

      var uri:String = null;
      if (uriStr == null) {
        uri = "csvline:"+reader.getLineNumber();
      } else {
        uri = uriStr;
      }
      assert (data != null)
      val carrier = new Instance(data, target, uri, uri)
      this.currentLine = reader.readLine()
      carrier
    }

    def hasNext()	= {	currentLine != null	}

    def remove()  = {
      throw new IllegalStateException ("This Iterator<Instance> does not support remove().");
    }

  }


  case class Config(command: String = "",
                    in_file: File = new File("."),
                    out_file: File = new File("."),
                    model_file: File = new File("."),
                    topics: Int = 100)

  def train(in_file:File, out_file:File, topics: Int) {
    val tok_pattern = Pattern.compile("[a-z]+")
    val pipes = new SerialPipes((Array(new CharSequence2TokenSequence(tok_pattern),
                                       new TokenSequence2FeatureSequence())))
    val li_pattern = Pattern.compile("(\\S+)\\w+(\\W.+)")
    val lit = new MyLI(in_file,
                       li_pattern, 2, -1, 1)
    val inst_list = new InstanceList(pipes)
    inst_list.addThruPipe(lit)

    val model = new ParallelTopicModel(topics, 50, 0.01)
    model.addInstances(inst_list)
    model.setTopicDisplay(50, 10)
    model.setNumIterations(5000)
    model.setOptimizeInterval(25)
    model.setBurninPeriod(200)
    model.setSymmetricAlpha(false)
    val threads = Runtime.getRuntime.availableProcessors
    model.setNumIterations(threads)
    model.estimate

    model.getTopWords(10).map(x => println(x.mkString(" ")))
    model.write(out_file)

  }

  def estimate(in_file:File, out_file:File, model_file:File, topics: Int) {
    val model = ParallelTopicModel.read(model_file)
    val inferencer = model.getInferencer()

    val tok_pattern = Pattern.compile("[a-z]+")
    val pipes = new SerialPipes((Array(new CharSequence2TokenSequence(tok_pattern),
                                       new TokenSequence2FeatureSequence())))
    val li_pattern = Pattern.compile("(\\S+)\\w+(\\W.+)")
    val lit = new MyLI(in_file,
                       li_pattern, 2, -1, 1)
    val inst_list = new InstanceList(pipes)
    inst_list.addThruPipe(lit)

    inferencer.writeInferredDistributions(inst_list, out_file, 500, 200, 200, 0, topics)
  }

  def main(args:Array[String]) {

    val parser = new scopt.OptionParser[Config]("scopt") {
      head("scopt", "3.x")
      cmd("train") action { (_, c) =>
        c.copy(command = "train") } text ("train the model.") children(
        arg[File]("in_file") action { (x, c) =>
          c.copy(in_file = x) } text("a tab separated input file."),
        arg[File]("out_file") action { (x, c) =>
          c.copy(out_file = x) } text("file name for model output"),
        opt[Int]('t', "topics") action { (x, c) =>
          c.copy(topics = x) } text("number of topics")
      )
      cmd("estimate") action {(_, c) =>
        c.copy(command = "estimate") } text ("estimate topics.") children(
        arg[File]("in_file") action { (x, c) =>
          c.copy(in_file = x) } text ("a tab separated input file."),
        arg[File]("model_file") action {(x, c) =>
          c.copy(model_file = x) } text("mallet lda model file"),
        arg[File]("out_file") action {(x, c) =>
          c.copy(out_file = x) } text("file name for estimated vectors"),
        opt[Int]('t', "topics") action { (x, c) =>
          c.copy(topics = x) } text("number of topics")
      )
    }

    parser.parse(args, Config()) match {
      case Some(config) => {
        config.command match {
          case "train" => train(config.in_file,
                                config.out_file,
                                config.topics)
          case "estimate" => estimate(config.in_file,
                                      config.out_file,
                                      config.model_file,
                                      config.topics)
          case _ =>
        }
      }
      case None =>
    }


  }

}
