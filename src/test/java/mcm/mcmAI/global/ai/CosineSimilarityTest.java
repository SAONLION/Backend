package mcm.mcmAI.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class CosineSimilarityTest {

    @Test
    void 완전히_같은_방향의_벡터는_유사도가_1이다() {
        float[] sessionVector = {1f, 2f, 3f};
        float[] productVector = {2f, 4f, 6f};

        double similarity = CosineSimilarity.compute(sessionVector, productVector);

        assertThat(similarity).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void 직교하는_벡터는_유사도가_0이다() {
        float[] sessionVector = {1f, 0f};
        float[] productVector = {0f, 1f};

        double similarity = CosineSimilarity.compute(sessionVector, productVector);

        assertThat(similarity).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void 반대_방향의_벡터는_유사도가_마이너스1이다() {
        float[] sessionVector = {1f, 1f};
        float[] productVector = {-1f, -1f};

        double similarity = CosineSimilarity.compute(sessionVector, productVector);

        assertThat(similarity).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void 유사도가_높은_제품_벡터일수록_상위로_정렬된다() {
        float[] sessionVector = {1f, 0f, 0f};
        float[] closeMatch = {0.9f, 0.1f, 0f};
        float[] partialMatch = {0.5f, 0.5f, 0f};
        float[] farMatch = {0f, 0f, 1f};

        double closeScore = CosineSimilarity.compute(sessionVector, closeMatch);
        double partialScore = CosineSimilarity.compute(sessionVector, partialMatch);
        double farScore = CosineSimilarity.compute(sessionVector, farMatch);

        assertThat(closeScore).isGreaterThan(partialScore);
        assertThat(partialScore).isGreaterThan(farScore);
    }

    @Test
    void 벡터_차원이_다르면_예외가_발생한다() {
        float[] sessionVector = {1f, 0f, 0f};
        float[] productVector = {1f, 0f};

        assertThatThrownBy(() -> CosineSimilarity.compute(sessionVector, productVector))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 영벡터와의_유사도는_0이다() {
        float[] sessionVector = {0f, 0f, 0f};
        float[] productVector = {1f, 2f, 3f};

        double similarity = CosineSimilarity.compute(sessionVector, productVector);

        assertThat(similarity).isZero();
    }
}
